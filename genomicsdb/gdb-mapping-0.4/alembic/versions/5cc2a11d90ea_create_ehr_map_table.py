#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create ehr_map table

Revision ID: 5cc2a11d90ea
Revises: eaff19134b73
Create Date: 2017-03-29 17:00:30.442477

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '5cc2a11d90ea'
down_revision = 'eaff19134b73'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        'ehr_map',
        sa.Column('id', sa.BigInteger, primary_key=True),
        sa.Column('ehr_map_id', sa.BigInteger, nullable=False),
        sa.Column('info', sa.Text)
    )

def downgrade():
    op.drop_table('ehr_map')
