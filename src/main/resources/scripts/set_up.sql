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
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',      -- 사용자 권한 (USER, ADMIN)
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id)
);

-- ============================================================
-- Places
-- ============================================================
CREATE TABLE places (
    id                  VARCHAR(50)   NOT NULL,               -- 카카오 장소 ID
    name                VARCHAR(255)  NOT NULL,
    category_name       VARCHAR(255)  NOT NULL,               -- 전체 카테고리 경로
    category_group_code VARCHAR(10)   NOT NULL,               -- FD6 | CE7
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
    id             CHAR(36)      NOT NULL DEFAULT (UUID()),
    user_id        CHAR(36)      NOT NULL,
    name           VARCHAR(100)  NOT NULL,
    default_group  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

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
    updated_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_comments_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

-- ============================================================
-- TasteTags (미식 태그 사전)
-- ============================================================
CREATE TABLE taste_tags (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    description  VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_taste_tags_code (code),
    CONSTRAINT chk_taste_tag_type CHECK (
        type IN ('CATEGORY', 'TASTE', 'MOOD', 'PURPOSE', 'PRICE', 'SERVICE')
    )
);

-- ============================================================
-- ReviewAnalysisStates (리뷰 미식 태그 분석 상태)
-- ============================================================
CREATE TABLE review_analysis_states (
    review_id          CHAR(36)     NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    content_hash       CHAR(64)     NOT NULL,
    extractor_version  VARCHAR(50)  NOT NULL,
    retry_count        INT          NOT NULL DEFAULT 0,
    next_retry_at      DATETIME,
    error_code         VARCHAR(50),
    error_message      VARCHAR(500),
    analyzed_at        DATETIME,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (review_id),
    KEY idx_review_analysis_retry (status, next_retry_at, updated_at),
    CONSTRAINT fk_review_analysis_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT chk_review_analysis_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'DEAD', 'SKIPPED')
    )
);

-- ============================================================
-- RecomputeTargets (다음 배치 계산 대상)
-- ============================================================
CREATE TABLE recompute_targets (
    target_type   VARCHAR(30) NOT NULL,
    target_id     VARCHAR(50) NOT NULL,
    requested_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (target_type, target_id),
    KEY idx_recompute_targets_requested (requested_at, target_type),
    CONSTRAINT chk_recompute_target_type CHECK (
        target_type IN ('REVIEW', 'PLACE', 'USER_TASTE', 'USER_RECOMMENDATION')
    )
);

-- ============================================================
-- ReviewTasteTags (리뷰별 추출 미식 태그)
-- ============================================================
CREATE TABLE review_taste_tags (
    review_id          CHAR(36)     NOT NULL,
    tag_id             BIGINT       NOT NULL,
    relevance          DECIMAL(5,4) NOT NULL,
    sentiment          DECIMAL(5,4) NOT NULL DEFAULT 0,
    source             VARCHAR(30)  NOT NULL DEFAULT 'FAKE',
    extractor_version  VARCHAR(50)  NOT NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (review_id, tag_id),
    KEY idx_review_taste_tags_tag (tag_id, review_id),
    CONSTRAINT fk_review_taste_tags_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_taste_tags_tag
        FOREIGN KEY (tag_id) REFERENCES taste_tags (id),
    CONSTRAINT chk_review_tag_relevance CHECK (relevance BETWEEN 0 AND 1),
    CONSTRAINT chk_review_tag_sentiment CHECK (sentiment BETWEEN -1 AND 1)
);

INSERT INTO taste_tags (code, name, type, description)
VALUES
    ('cafe', '카페', 'CATEGORY', '카페형 장소'),
    ('dessert', '디저트', 'CATEGORY', '디저트와 베이커리'),
    ('bakery', '베이커리', 'CATEGORY', '빵과 베이커리'),
    ('korean', '한식', 'CATEGORY', '한식'),
    ('japanese', '일식', 'CATEGORY', '일식'),
    ('western', '양식', 'CATEGORY', '양식'),
    ('spicy', '매운맛', 'TASTE', '매운맛'),
    ('sweet', '단맛', 'TASTE', '달콤한 맛'),
    ('savory', '고소함', 'TASTE', '고소한 맛'),
    ('light', '가벼움', 'TASTE', '가볍고 부담 없는 맛'),
    ('rich', '진한맛', 'TASTE', '진하고 묵직한 맛'),
    ('quiet', '조용함', 'MOOD', '조용한 분위기'),
    ('lively', '활기참', 'MOOD', '활기찬 분위기'),
    ('cozy', '아늑함', 'MOOD', '아늑한 분위기'),
    ('clean', '청결함', 'MOOD', '청결한 공간'),
    ('date', '데이트', 'PURPOSE', '데이트에 적합'),
    ('family', '가족', 'PURPOSE', '가족 방문에 적합'),
    ('group', '모임', 'PURPOSE', '단체나 모임에 적합'),
    ('solo', '혼밥', 'PURPOSE', '혼자 방문하기 좋음'),
    ('value', '가성비', 'PRICE', '가격 대비 만족도'),
    ('premium', '프리미엄', 'PRICE', '비싸지만 특별함'),
    ('kind_service', '친절함', 'SERVICE', '친절한 응대'),
    ('waiting', '웨이팅', 'SERVICE', '대기와 줄'),
    ('parking', '주차', 'SERVICE', '주차 편의')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    description = VALUES(description),
    active = TRUE;

-- ============================================================
-- Images (이미지 업로드 및 생명주기 관리)
-- ============================================================
CREATE TABLE images (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    filename    VARCHAR(255)  NOT NULL,
    url         VARCHAR(255)  NOT NULL,
    status      VARCHAR(50)   NOT NULL COMMENT 'PENDING 또는 CONFIRMED',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Indexes (성능 최적화)
-- ============================================================

-- 스케줄러 PENDING 이미지 조회 최적화용 복합 인덱스
CREATE INDEX idx_images_status_created_at ON images (status, created_at);

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

