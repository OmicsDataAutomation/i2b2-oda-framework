"""change ehr map type

Revision ID: 0dcdffe12507
Revises: 9b08d09cf8d1
Create Date: 2018-07-11 03:16:48.051815

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '0dcdffe12507'
down_revision = '9b08d09cf8d1'
branch_labels = None
depends_on = None


def upgrade():
    op.alter_column("ehr_map", "ehr_map_id", type_=sa.Text)


def downgrade():
    op.alter_column("ehr_map", "ehr_map_id", type=sa.BigInteger)
