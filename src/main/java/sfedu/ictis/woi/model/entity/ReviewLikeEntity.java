package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sfedu.ictis.woi.model.entity.id.ReviewLikeId;

@Entity
@Table(name = "review_likes")
@Getter
@Setter
@IdClass(ReviewLikeId.class)
public class ReviewLikeEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private ReviewEntity review;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private Boolean value;
}