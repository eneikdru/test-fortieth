-- V20260731145730246__create_tasks_table.sql
-- Create table for workflow and pipeline tasks, pre-populating the stalled task.

CREATE TABLE tasks (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert the stalled task to fulfill acceptance criteria
INSERT INTO tasks (id, title, status)
VALUES ('529e5252-040a-4889-9f61-366ea6e9e089', 'Stalled pipeline review task', 'pending_review');
