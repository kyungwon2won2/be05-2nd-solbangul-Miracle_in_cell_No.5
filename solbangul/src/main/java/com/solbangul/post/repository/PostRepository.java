package com.solbangul.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.solbangul.post.domain.Category;
import com.solbangul.post.domain.Post;
import com.solbangul.room.domain.Room;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("select p from Post p where p.room.id = :roomId")
	List<Post> findPostsByRoomId(@Param("roomId") Long roomId);

	@Query("select p from Post p where p.writer = :writer and p.room = :room and p.category = :category order by p.createdDate desc limit 1")
	Post findLastPost(@Param("writer") String writer, @Param("room") Room room, @Param("category") Category category);

	
	@Query("select p from Post p where p.writer = :writer")
	List<Post> findAllByWriter(@Param("writer") String writer);

	//조회수 증가
	@Modifying
	@Transactional
	@Query("update Post p set viewCount=viewCount+1 where p.id = :postId")
	void updateByCnt(@Param("postId") Long postId);

}
