package com.sparta.eatsapp.review.entity;

import com.sparta.eatsapp.common.Timestamped;
import com.sparta.eatsapp.review.dto.ReviewRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "review")
public class Review extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewId")
    private Long reviewId;

    @Column(name = "orderId", nullable = false, length = 20)
    private Long orderId;

    @Column(name = "content", nullable = false, length = 100)
    private String content;

    @Column(name = "star", nullable = false, length = 10)
    private int star;

    public Review(ReviewRequestDto requestDto) {
        this.orderId = requestDto.getOrderId();
        this.content = requestDto.getContent();
        this.star = requestDto.getStar();
    }
}