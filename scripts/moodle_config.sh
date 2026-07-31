#!/bin/bash

# ==============================================================================
# Moodle Configuration Script
# Scaffolds the UI dashboard for financial reporting and Epidemiology knowledge base
# Reference: Original Brief & docs/architecture/moodle-financial-spike.md
# ==============================================================================

set -e

echo "Starting Moodle Knowledge Base and Financial Dashboard configuration..."

# 1. Ensure Categories
echo "Ensuring Categories..."
# Category 1: Financial block
# Category 2: Epidemiology Knowledge Base (ФБУН ЦНИИ Эпидемиологии)
# Assuming categories are created or fetched via moosh (these commands map to logical requirements)

# 2. Create Knowledge Base Course (Epidemiology)
echo "Creating Epidemiology Knowledge Base Course..."
moosh course-create --category 2 "База знаний ФБУН ЦНИИ Эпидемиологии" "EPID_KB"

# 3. Add Epidemiology Books and Pages
# Books for nested content
moosh activity-add --course EPID_KB book "Нормативная база (ФГОС, Приказы)"
moosh activity-add --course EPID_KB book "Подготовка к аттестациям (ГИА)"
moosh activity-add --course EPID_KB page "Шаблоны документов (ГЭК)"

# 4. Create Financial Administration Course (per Spike & AC)
echo "Creating Financial Administration Course..."
moosh course-create --category 1 "Financial Administration 2026" "FIN2026"

# 5. Add Financial Books, Pages, and Resources
moosh activity-add --course FIN2026 book "Annual Budget 2026"
moosh activity-add --course FIN2026 book "Instructor Workloads"
moosh activity-add --course FIN2026 page "Q1 Financial Summary"
moosh activity-add --course FIN2026 resource "budget_template_v3.xlsx"

echo "Moodle UI dashboard configuration scaffolds generated successfully."
