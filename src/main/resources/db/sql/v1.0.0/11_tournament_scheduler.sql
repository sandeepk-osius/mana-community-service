-- FILE: db/sql/v1.0.0/11_tournament_scheduler.sql

CREATE TABLE IF NOT EXISTS tournament_config (
    id                          BIGSERIAL    PRIMARY KEY,
    tournament_name             VARCHAR(150) NOT NULL,
    sport_id                    BIGINT       REFERENCES sport_meta(id),
    community_id                BIGINT       REFERENCES community(id),
    event_id                    BIGINT       REFERENCES sports_event(id),
    tournament_type             VARCHAR(30)  NOT NULL,
    total_teams                 INT          NOT NULL,
    number_of_groups            INT,
    teams_per_group             INT,
    teams_advancing_per_group   INT,
    third_place_match           BOOLEAN      DEFAULT TRUE,
    has_seeding                 BOOLEAN      DEFAULT FALSE,
    swiss_rounds                INT,
    start_date                  DATE         NOT NULL,
    end_date                    DATE,
    match_duration_minutes      INT          DEFAULT 90,
    break_between_matches_minutes INT        DEFAULT 30,
    venue_name                  VARCHAR(200),
    points_for_win              INT          DEFAULT 2,
    points_for_draw             INT          DEFAULT 1,
    points_for_loss             INT          DEFAULT 0,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by                  BIGINT       REFERENCES app_user(id),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tournament_type CHECK (tournament_type IN (
        'KNOCKOUT','GROUP_KNOCKOUT','ROUND_ROBIN',
        'DOUBLE_ELIMINATION','SWISS','SUPER_LEAGUE',
        'KNOCKOUT_SINGLE','KNOCKOUT_DOUBLE',
        'GROUP_PLAYOFF','LEAGUE','CUSTOM'
    )),
    CONSTRAINT chk_tournament_status CHECK (status IN (
        'DRAFT','ACTIVE','LIVE','COMPLETED','CANCELLED'
    ))
);

CREATE TABLE IF NOT EXISTS tournament_group (
    id          BIGSERIAL   PRIMARY KEY,
    config_id   BIGINT      NOT NULL REFERENCES tournament_config(id) ON DELETE CASCADE,
    group_name  VARCHAR(20) NOT NULL,
    group_order INT         NOT NULL,
    CONSTRAINT uq_group_config UNIQUE (config_id, group_name)
);

CREATE TABLE IF NOT EXISTS group_team_standing (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT    NOT NULL REFERENCES tournament_group(id) ON DELETE CASCADE,
    team_id         BIGINT    NOT NULL REFERENCES auction_team(id),
    seed_rank       INT,
    played          INT       DEFAULT 0,
    won             INT       DEFAULT 0,
    lost            INT       DEFAULT 0,
    drawn           INT       DEFAULT 0,
    points          INT       DEFAULT 0,
    runs_for        INT       DEFAULT 0,
    runs_against    INT       DEFAULT 0,
    overs_for       INT       DEFAULT 0,
    overs_against   INT       DEFAULT 0,
    net_run_rate    DECIMAL(8,3) DEFAULT 0.000,
    qualified       BOOLEAN   DEFAULT FALSE,
    eliminated      BOOLEAN   DEFAULT FALSE,
    CONSTRAINT uq_standing_group_team UNIQUE (group_id, team_id)
);

CREATE TABLE IF NOT EXISTS tournament_match (
    id                          BIGSERIAL    PRIMARY KEY,
    config_id                   BIGINT       NOT NULL REFERENCES tournament_config(id),
    group_id                    BIGINT       REFERENCES tournament_group(id),
    round                       VARCHAR(30),
    round_number                INT,
    match_number                INT,
    bracket_slot                INT,
    team_a_id                   BIGINT       REFERENCES auction_team(id),
    team_b_id                   BIGINT       REFERENCES auction_team(id),
    winner_feed_from_match_a    BIGINT       REFERENCES tournament_match(id),
    winner_feed_from_match_b    BIGINT       REFERENCES tournament_match(id),
    winner_advances_to_match_id BIGINT       REFERENCES tournament_match(id),
    loser_sent_to_match_id      BIGINT       REFERENCES tournament_match(id),
    scheduled_at                TIMESTAMP    NOT NULL,
    duration_minutes            INT          DEFAULT 90,
    venue_name                  VARCHAR(200),
    court_number                VARCHAR(10),
    status                      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    score_team_a                VARCHAR(50),
    score_team_b                VARCHAR(50),
    winner_team_id              BIGINT       REFERENCES auction_team(id),
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP,
    match_notes                 TEXT,
    swiss_round_number          INT,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_match_status CHECK (status IN (
        'SCHEDULED','LIVE','COMPLETED','POSTPONED','CANCELLED','BYE'
    ))
);

-- Indexes
CREATE INDEX idx_tm_config_status   ON tournament_match(config_id, status);
CREATE INDEX idx_tm_scheduled_at    ON tournament_match(scheduled_at);
CREATE INDEX idx_tm_round           ON tournament_match(config_id, round_number);
CREATE INDEX idx_tm_group           ON tournament_match(group_id);
CREATE INDEX idx_standing_group     ON group_team_standing(group_id, points DESC, net_run_rate DESC);
