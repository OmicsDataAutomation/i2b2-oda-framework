#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create gene table

Revision ID: ba7e2f758e85
Revises: 52ea1ac359b0
Create Date: 2017-03-27 16:36:37.514319

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'ba7e2f758e85'
down_revision = '52ea1ac359b0'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
	'gene',
	sa.Column('id', sa.BigInteger, primary_key=True),
	sa.Column('contig_id', sa.BigInteger, sa.ForeignKey('contig.id'), nullable=False),
	sa.Column('name', sa.Text, nullable=False),
	sa.Column('start', sa.BigInteger, nullable=False),
	sa.Column('end', sa.BigInteger, nullable=False),
	sa.Column('description', sa.Text)
    )

def downgrade():
    op.drop_table('gene')
