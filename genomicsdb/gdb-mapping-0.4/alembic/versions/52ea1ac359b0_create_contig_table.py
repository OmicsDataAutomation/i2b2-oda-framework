#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create contig table

Revision ID: 52ea1ac359b0
Revises: 985d02a40a50
Create Date: 2017-03-27 16:19:56.990397

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '52ea1ac359b0'
down_revision = '985d02a40a50'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
	'contig',
	sa.Column('id', sa.BigInteger, primary_key=True),
	sa.Column('reference_id', sa.BigInteger, sa.ForeignKey('reference.id'), nullable=False),
	sa.Column('name', sa.Text, nullable=False),
	sa.Column('length', sa.BigInteger, nullable=False),
	sa.Column('tiledb_column_offset', sa.BigInteger)
    )


def downgrade():
    op.drop_table('contig')
