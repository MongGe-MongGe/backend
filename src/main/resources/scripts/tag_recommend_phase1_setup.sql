-- ============================================================
-- Tag recommendation phase 1 setup (existing development DB)
-- ============================================================

USE gourming;

-- ============================================================
-- TasteTags (미식 태그 사전)
-- ============================================================
CREATE TABLE IF NOT EXISTS taste_tags (
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
CREATE TABLE IF NOT EXISTS review_analysis_states (
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
CREATE TABLE IF NOT EXISTS recompute_targets (
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
CREATE TABLE IF NOT EXISTS review_taste_tags (
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
