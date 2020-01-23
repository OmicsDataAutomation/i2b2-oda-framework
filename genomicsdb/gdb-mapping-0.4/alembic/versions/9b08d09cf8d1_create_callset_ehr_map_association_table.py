#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create callset ehr map association table

Revision ID: 9b08d09cf8d1
Revises: 5cc2a11d90ea
Create Date: 2017-03-29 17:06:40.256692

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '9b08d09cf8d1'
down_revision = '5cc2a11d90ea'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        'callset_ehr_map_association',
        sa.Column('callset_id', sa.BigInteger, sa.ForeignKey('callset.id'), primary_key=True),
        sa.Column('ehr_map_id', sa.BigInteger, sa.ForeignKey('ehr_map.id'), primary_key=True),
        sa.ForeignKeyConstraint(['callset_id'],['callset.id'],),
        sa.ForeignKeyConstraint(['ehr_map_id'],['ehr_map.id'],),
        sa.PrimaryKeyConstraint('callset_id', 'ehr_map_id', name='primary_key_ehr')
    )

def downgrade():
    op.drop_table('callset_ehr_map_association')
