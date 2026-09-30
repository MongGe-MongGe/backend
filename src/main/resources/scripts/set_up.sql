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
-- Password Reset Tokens
-- ============================================================
CREATE TABLE password_reset_tokens (
    id          CHAR(36)      NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)      NOT NULL,
    token_hash  VARCHAR(255)  NOT NULL UNIQUE,
    expires_at  DATETIME      NOT NULL,
    used_at     DATETIME      NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);

CREATE INDEX idx_password_reset_tokens_expires_at
    ON password_reset_tokens (expires_at);

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
-- Place Review Summaries (장소별 리뷰 AI 요약)
-- ============================================================
CREATE TABLE place_review_summaries (
    place_id                VARCHAR(50)   NOT NULL,
    summary                 TEXT          NULL,
    positive_points         JSON          NULL,
    negative_points         JSON          NULL,
    recommended_for         JSON          NULL,
    keywords                JSON          NULL,
    tag_sentiments          JSON          NULL,                -- {"dessert": 0.9, "waiting": -0.8}
    review_count            INT           NOT NULL DEFAULT 0,
    model_version           VARCHAR(50)   NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    last_review_updated_at  DATETIME      NULL,
    error_message           VARCHAR(500)  NULL,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_review_summaries_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
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
-- PopularReviewScores (인기피드 집계 결과)
-- ============================================================
CREATE TABLE popular_review_scores (
    review_id      CHAR(36)       NOT NULL,
    score          DECIMAL(10, 4) NOT NULL,
    like_count     BIGINT         NOT NULL DEFAULT 0,
    comment_count  BIGINT         NOT NULL DEFAULT 0,
    rank_no        INT            NOT NULL,
    window_days    INT            NOT NULL,
    calculated_at  DATETIME       NOT NULL,

    PRIMARY KEY (window_days, review_id),
    UNIQUE KEY uq_popular_review_rank (window_days, rank_no),
    KEY idx_popular_review_score (window_days, score DESC),
    CONSTRAINT fk_popular_review_scores_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

-- ============================================================
-- ReviewEmbeddings (리뷰 본문 임베딩 벡터)
-- ============================================================
CREATE TABLE review_embeddings (
    review_id         CHAR(36)     NOT NULL,
    embedding         JSON         NOT NULL,                  -- [0.01, -0.02, ...]
    embedder_version  VARCHAR(50)  NOT NULL,                  -- 모델명 또는 fake-v1
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (review_id),
    CONSTRAINT fk_review_embeddings_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);

-- ============================================================
-- PlaceEmbeddings (장소 벡터: 리뷰 벡터 평균 또는 카테고리 임베딩)
-- ============================================================
CREATE TABLE place_embeddings (
    place_id          VARCHAR(50)  NOT NULL,
    embedding         JSON         NOT NULL,
    source            VARCHAR(10)  NOT NULL,                  -- REVIEWS | CATEGORY
    review_count      INT          NOT NULL DEFAULT 0,
    embedder_version  VARCHAR(50)  NOT NULL,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_embeddings_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
);

-- ============================================================
-- TasteTags (미식 태그 사전)
-- ============================================================
CREATE TABLE taste_tags (
    code              VARCHAR(30)   NOT NULL,                 -- dessert, quiet, ...
    label             VARCHAR(50)   NOT NULL,                 -- 화면 표시명
    description       VARCHAR(200)  NOT NULL,                 -- 임베딩 대상 설명문
    category          VARCHAR(20)   NOT NULL,                 -- CATEGORY | TASTE | MOOD | PURPOSE | PRICE | SERVICE
    embedding         JSON          NULL,                     -- 배치가 채운다
    embedder_version  VARCHAR(50)   NULL,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order        INT           NOT NULL DEFAULT 0,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (code)
);

INSERT INTO taste_tags (code, label, description, category, sort_order) VALUES
    ('cafe',     '카페',          '커피와 음료를 마시며 머무르기 좋은 카페',            'CATEGORY', 10),
    ('dessert',  '디저트',        '케이크, 빵, 디저트 메뉴가 맛있는 곳',                'CATEGORY', 11),
    ('bakery',   '베이커리',      '갓 구운 빵과 페이스트리를 파는 빵집',                'CATEGORY', 12),
    ('korean',   '한식',          '국밥, 찌개, 김치 같은 한식을 내는 식당',              'CATEGORY', 13),
    ('japanese', '일식',          '라멘, 초밥, 돈카츠 같은 일식을 내는 식당',            'CATEGORY', 14),
    ('western',  '양식',          '파스타, 스테이크, 피자 같은 양식을 내는 식당',        'CATEGORY', 15),
    ('chinese',  '중식',          '짜장면, 탕수육, 마라 같은 중식을 내는 식당',          'CATEGORY', 16),
    ('spicy',    '매운맛',        '매운, 매콤한, 얼큰한 음식이 특징인 곳',               'TASTE',    20),
    ('sweet',    '달콤한 맛',     '달콤한 단맛이 특징인 음식과 디저트',                  'TASTE',    21),
    ('savory',   '고소한 맛',     '고소하고 감칠맛이 진한 음식',                         'TASTE',    22),
    ('light',    '담백한 맛',     '담백하고 깔끔한 맛의 음식',                           'TASTE',    23),
    ('rich',     '진한 맛',       '진하고 묵직한 국물이나 소스가 특징인 음식',           'TASTE',    24),
    ('quiet',    '조용한 분위기', '조용하고 차분해서 대화나 작업에 좋은 분위기',         'MOOD',     30),
    ('lively',   '활기찬 분위기', '활기차고 시끌벅적한 분위기',                          'MOOD',     31),
    ('cozy',     '아늑한 분위기', '아늑하고 편안한 인테리어와 좌석',                     'MOOD',     32),
    ('clean',    '청결',          '청결하고 깨끗하게 관리되는 매장',                     'MOOD',     33),
    ('view',     '뷰 맛집',       '뷰와 전망이 좋은 자리가 있는 곳',                     'MOOD',     34),
    ('date',     '데이트',        '연인과 데이트하기 좋은 곳',                           'PURPOSE',  40),
    ('family',   '가족 모임',     '가족이나 아이와 함께 가기 좋은 곳',                   'PURPOSE',  41),
    ('group',    '단체 모임',     '여러 명이 모임이나 회식을 하기 좋은 곳',              'PURPOSE',  42),
    ('solo',     '혼밥',          '혼자 가서 혼밥하기 편한 곳',                          'PURPOSE',  43),
    ('value',    '가성비',        '가격이 저렴하고 가성비가 좋은 곳',                    'PRICE',    50),
    ('premium',  '프리미엄',      '가격대가 높은 고급 프리미엄 식당',                    'PRICE',    51),
    ('kind',     '친절',          '직원이 친절하고 응대가 좋은 곳',                      'SERVICE',  60),
    ('waiting',  '웨이팅',        '대기 줄이 길고 웨이팅이 필요한 곳',                   'SERVICE',  61),
    ('parking',  '주차',          '주차가 편리하거나 주차 공간이 있는 곳',               'SERVICE',  62);

-- ============================================================
-- ReviewImpressions (리뷰 노출 기록: 사용자가 피드에서 실제로 본 리뷰)
-- ============================================================
CREATE TABLE review_impressions (
    user_id        CHAR(36)  NOT NULL,
    review_id      CHAR(36)  NOT NULL,
    first_seen_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_count     INT       NOT NULL DEFAULT 1,

    PRIMARY KEY (user_id, review_id),
    KEY idx_review_impressions_user_seen (user_id, last_seen_at),
    CONSTRAINT fk_review_impressions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_impressions_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
);


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

