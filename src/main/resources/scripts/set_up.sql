-- ============================================================
-- Gourming Database Schema (MySQL)
-- ============================================================

CREATE DATABASE IF NOT EXISTS gourming CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gourming;

-- ============================================================
-- Users
-- ============================================================
CREATE TABLE users (
    id            CHAR(36)      NOT NULL DEFAULT (UUID()),
    email         VARCHAR(255)  NOT NULL UNIQUE,
    password      VARCHAR(255)  NOT NULL,                     -- bcrypt hashed
    handle        VARCHAR(50)   NOT NULL UNIQUE,              -- @... 형식
    nickname      VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20),                                -- 전화번호 추가
    bio           TEXT,
    profile_image VARCHAR(500),
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id)
);

-- ============================================================
-- Places
-- ============================================================
CREATE TABLE places (
    id                  VARCHAR(50)   NOT NULL,               -- 카카오 장소 ID
    name                VARCHAR(255)  NOT NULL,
    category_name       VARCHAR(10)   NOT NULL,               -- FD6 | CE7
    road_address_name   VARCHAR(500)  NOT NULL,
    x                   VARCHAR(50)   NOT NULL,               -- 경도 (longitude)
    y                   VARCHAR(50)   NOT NULL,               -- 위도 (latitude)
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
);

-- ============================================================
-- Reviews
-- ============================================================
CREATE TABLE reviews (
    id            CHAR(36)      NOT NULL DEFAULT (UUID()),
    content       TEXT          NOT NULL,
    images        JSON,                                       -- ["url1", "url2", ...]
    rating_score  TINYINT UNSIGNED,                          -- 1~5, NULL 허용
    visited_at    DATE,                                      -- 방문일, NULL 허용
    place_id      VARCHAR(50)   NOT NULL,
    user_id       CHAR(36)      NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_place FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT chk_rating CHECK (rating_score BETWEEN 1 AND 5)
);

-- ============================================================
-- Likes (리뷰 좋아요)
-- ============================================================
CREATE TABLE likes (
    id          CHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)  NOT NULL,
    review_id   CHAR(36)  NOT NULL,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_likes_user_review (user_id, review_id),    -- 중복 좋아요 방지
    CONSTRAINT fk_likes_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_likes_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

-- ============================================================
-- Follows
-- ============================================================
CREATE TABLE follows (
    id              CHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id         CHAR(36)  NOT NULL,                      -- 팔로우 하는 유저
    target_user_id  CHAR(36)  NOT NULL,                      -- 팔로우 당하는 유저
    created_at      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_follows (user_id, target_user_id),         -- 중복 팔로우 방지
    CONSTRAINT chk_no_self_follow CHECK (user_id != target_user_id),
    CONSTRAINT fk_follows_user        FOREIGN KEY (user_id)        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_target_user FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- Groups (굿플레이스 그룹)
-- ============================================================
CREATE TABLE `groups` (
    id          CHAR(36)      NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)      NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_groups_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- GoodPlaces (저장한 장소)
-- ============================================================
CREATE TABLE good_places (
    id          CHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)  NOT NULL,
    group_id    CHAR(36)  NOT NULL,
    place_id    VARCHAR(50) NOT NULL,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_good_places (user_id, group_id, place_id), -- 동일 그룹에 중복 저장 방지
    CONSTRAINT fk_good_places_user  FOREIGN KEY (user_id)  REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_good_places_group FOREIGN KEY (group_id) REFERENCES `groups` (id) ON DELETE CASCADE,
    CONSTRAINT fk_good_places_place FOREIGN KEY (place_id) REFERENCES places (id)   ON DELETE CASCADE
);

-- ============================================================
-- Posts (공지 / 이벤트 게시글)
-- ============================================================
CREATE TABLE posts (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)     NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(20)  NOT NULL,                       -- Notice | Event
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT chk_post_category CHECK (category IN ('Notice', 'Event')),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- Comments (리뷰 댓글)
-- ============================================================
CREATE TABLE comments (
    id          CHAR(36)  NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)  NOT NULL,
    review_id   CHAR(36)  NOT NULL,
    content     TEXT      NOT NULL,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_comments_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

-- ============================================================
-- Indexes (성능 최적화)
-- ============================================================

-- 리뷰 조회
CREATE INDEX idx_reviews_place_id  ON reviews (place_id);
CREATE INDEX idx_reviews_user_id   ON reviews (user_id);
CREATE INDEX idx_reviews_created_at ON reviews (created_at DESC);

-- 좋아요 집계
CREATE INDEX idx_likes_review_id ON likes (review_id);

-- 팔로우 피드용
CREATE INDEX idx_follows_user_id        ON follows (user_id);
CREATE INDEX idx_follows_target_user_id ON follows (target_user_id);

-- 굿플레이스 조회
CREATE INDEX idx_good_places_user_id  ON good_places (user_id);
CREATE INDEX idx_good_places_group_id ON good_places (group_id);

-- 댓글 조회
CREATE INDEX idx_comments_review_id ON comments (review_id);

