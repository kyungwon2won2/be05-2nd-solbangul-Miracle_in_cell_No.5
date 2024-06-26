package com.solbangul.post.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.solbangul.post.comment.domain.Comment;
import com.solbangul.post.comment.repository.CommentRepository;
import com.solbangul.post.domain.dto.PostEditRequestDto;
import com.solbangul.post.domain.dto.PostViewResponseDto;
import com.solbangul.post.domain.dto.PostsSaveRequestDto;
import com.solbangul.post.service.PostService;
import com.solbangul.user.domain.User;
import com.solbangul.user.domain.dto.CustomUserDetails;
import com.solbangul.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/room/{room_id}/post")
@RequiredArgsConstructor
@Slf4j
public class PostController {

	private final PostService postService;
	private final UserService userService;
	private final CommentRepository commentRepository;

	@GetMapping("/save")
	public String getSave(@PathVariable(name = "room_id") Long id, Model model,
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		String nickname = customUserDetails.getAuthenticatedUser().getNickname();
		PostsSaveRequestDto post = new PostsSaveRequestDto();
		post.setWriter(nickname);

		model.addAttribute("room_id", id);
		model.addAttribute("post", post);
		return "save_post";
	}

	@PostMapping("/save")
	public String postsSave(@PathVariable(name = "room_id") Long roomId, PostsSaveRequestDto requestDto,
		@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		Long userId = customUserDetails.getAuthenticatedUser().getId();
		User authenticatedUser = userService.findOne(userId);

		requestDto.setWriter(authenticatedUser.getNickname());
		requestDto.setReadYn(false);
		requestDto.setRoomId(roomId);
		requestDto.setUserId(authenticatedUser.getId());

		// 게시물 저장
		postService.save(requestDto);
		return "redirect:/room/" + roomId + "/view";
	}

	@GetMapping("/{post_id}/view")
	public String postsView(@PathVariable(name = "room_id") Long room_id, @PathVariable(name = "post_id") Long post_id,
		Model model, @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		PostViewResponseDto postDto = postService.findById(post_id);

		Long userId = customUserDetails.getAuthenticatedUser().getId();
		User authenticatedUser = userService.findOne(userId);

		List<Comment> comments = commentRepository.findByPostId(post_id);

		model.addAttribute("room_id", room_id);
		model.addAttribute("post_id", post_id);
		model.addAttribute("postInfo", postDto);
		model.addAttribute("userInfo", authenticatedUser);
		model.addAttribute("comments", comments);

		postService.updateViewCountById(post_id);

		return "view_post";
	}

	@GetMapping("/{post_id}/edit")
	public String updateForm(@PathVariable(name = "room_id") Long room_id, @PathVariable(name = "post_id") Long post_id,
		@AuthenticationPrincipal CustomUserDetails customUserDetails,
		Model model) {
		PostEditRequestDto dto = postService.editFindById(post_id);
		dto.setWriter(customUserDetails.getAuthenticatedUser().getName());
		model.addAttribute("room_id", room_id);
		model.addAttribute("post_id", post_id);
		model.addAttribute("postInfo", dto);
		return "edit_post";
	}

	@PostMapping("/{post_id}/edit") //submit 눌렀을 때 ,,
	public String update(@PathVariable(name = "room_id") Long room_id, @PathVariable(name = "post_id") Long post_id,
		PostEditRequestDto requestDto) {
		postService.update(post_id, requestDto);
		return "redirect:/room/" + room_id + "/post/" + post_id + "/view";
	}

	@RequestMapping(value = "/{post_id}/delete", method = {RequestMethod.GET, RequestMethod.POST})
	public String delete(@PathVariable(name = "room_id") Long room_id, @PathVariable(name = "post_id") Long post_id) {
		postService.delete(post_id);
		return "redirect:/room/" + room_id + "/view_posts";
	}

}
