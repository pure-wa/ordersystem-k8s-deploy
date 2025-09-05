package lazyteam.cooking_hansu.domain.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 상호작용 상태 DTO (좋아요 + 북마크)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostInteractionStatusDto {
    
    private boolean isLiked;        // 현재 사용자의 좋아요 상태
    private boolean isBookmarked;   // 현재 사용자의 북마크 상태
    private Long likeCount;         // 총 좋아요 수
    private Long bookmarkCount;     // 총 북마크 수
    private Long viewCount;         // 총 조회수
}
