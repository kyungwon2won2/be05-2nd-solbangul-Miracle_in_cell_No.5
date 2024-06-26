package com.solbangul.post.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.solbangul.post.comment.domain.Comment;
import com.solbangul.post.comment.repository.CommentRepository;
import com.solbangul.post.domain.Category;
import com.solbangul.post.domain.Post;
import com.solbangul.post.domain.dto.PostEditRequestDto;
import com.solbangul.post.domain.dto.PostFindByRoomListResponseDto;
import com.solbangul.post.domain.dto.PostSearchListResponseDto;
import com.solbangul.post.domain.dto.PostViewResponseDto;
import com.solbangul.post.domain.dto.PostsSaveRequestDto;
import com.solbangul.post.repository.PostRepository;
import com.solbangul.post.specification.PostSpecification;
import com.solbangul.room.domain.Room;
import com.solbangul.room.repository.RoomRepository;
import com.solbangul.user.domain.User;
import com.solbangul.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

	public static final int COMPLIMENT_AMOUNT = 3;
	public static final int WRITE_AMOUNT = 1;

	private final PostRepository postRepository;
	private final RoomRepository roomRepository;
	private final UserRepository userRepository;
	private final CommentRepository commentRepository;

	@Transactional
	public Long save(PostsSaveRequestDto requestDto) {
		Room receiverRoom = roomRepository.findById(requestDto.getRoomId()).orElseThrow();

		User receiverUser = receiverRoom.getUser();
		User senderUser = userRepository.findById(requestDto.getUserId()).orElseThrow();

		Post post = requestDto.toEntity(receiverRoom);

		Long sederUserId = senderUser.getId();
		Category category = post.getCategory();
		Post lastPost = postRepository.findLastPost(sederUserId, receiverRoom, category);

		if (postEmpty(lastPost)) {
			addSolbangulSenderAndReceiver(post, receiverUser, senderUser);
			return postRepository.save(post).getId();
		}

		LocalDateTime lastPostDateTime = lastPost.getCreatedDate();
		if (!isLastPostToday(lastPostDateTime)) {
			addSolbangulSenderAndReceiver(post, receiverUser, senderUser);
		}

		return postRepository.save(post).getId();
	}

	private static boolean postEmpty(Post lastPost) {
		return lastPost == null;
	}

	private static void addSolbangulSenderAndReceiver(Post post, User receiverUser, User senderUser) {
		if (post.getCategory().equals(Category.COMPLIMENT)) {
			receiverUser.addSolbangul(COMPLIMENT_AMOUNT);
		}
		senderUser.addSolbangul(WRITE_AMOUNT);
	}

	private static boolean isLastPostToday(LocalDateTime lastPostDateTime) {
		return lastPostDateTime.toLocalDate().isEqual(LocalDate.now());
	}

	// 한 회원의 방
	public PostViewResponseDto findById(Long id) {
		Post post = postRepository.findById(id).orElseThrow(()
			-> new IllegalArgumentException("해당 room이 없습니다. id=" + id));

		PostViewResponseDto postViewResponseDto = new PostViewResponseDto(post);
		postViewResponseDto.setPostUserRoomId(post.getRoom().getId());
		return postViewResponseDto;
	}

	public Page<PostFindByRoomListResponseDto> findPostsByRoomId(Long id, Pageable pageable) {
		Page<Post> posts = postRepository.findPostsByRoomIdPaging(id, pageable);
		return posts.map(PostFindByRoomListResponseDto::new);
	}

	@Transactional
	public Page<PostSearchListResponseDto> search(String keyword, String category, Long roomId, Pageable pageable) {

		Specification<Post> spec = Specification.where(PostSpecification.likeContents(keyword));
		spec = spec.or(PostSpecification.likeTitle(keyword));
		spec = spec.or(PostSpecification.likeWriter(keyword));
		spec = spec.and(PostSpecification.findByRoomId(roomId));

		if (category.equals("compliment")) {
			spec = spec.and(PostSpecification.findByCategory(Category.COMPLIMENT));
		} else if (category.equals("claims")) {
			spec = spec.and(PostSpecification.findByCategory(Category.CLAIMS));
		} else if (category.equals("free")) {
			spec = spec.and(PostSpecification.findByCategory(Category.FREE));
		}

		Page<Post> posts = postRepository.findAll(spec, pageable);

		return posts.map(PostSearchListResponseDto::new);
	}

	public PostEditRequestDto editFindById(Long id) {
		Post post = postRepository.findById(id).orElseThrow(()
			-> new IllegalArgumentException("해당 post가 없습니다. id=" + id));

		return new PostEditRequestDto(post);
	}

	@Transactional
	public void update(Long id, PostEditRequestDto requestDto) {
		Post post = postRepository.findById(id).orElseThrow(()
			-> new IllegalArgumentException("해당 room이 없습니다. id=" + id));
		post.update(requestDto.getTitle(), requestDto.getPublicYn(), requestDto.getAnnonyYn(),
			requestDto.getContent(),
			requestDto.getCategory());
	}

	@Transactional
	public void delete(Long id) {
		Post post = postRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id " + id));
		List<Comment> comments = commentRepository.findByPostId(post.getId());
		commentRepository.deleteAll(comments);
		postRepository.delete(post);
	}

	@Transactional
	public void updateViewCountById(Long id) {
		postRepository.updateByCount(id);
	}
}
