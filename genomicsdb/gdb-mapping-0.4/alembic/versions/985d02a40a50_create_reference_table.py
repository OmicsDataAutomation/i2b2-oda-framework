#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create reference table

Revision ID: 985d02a40a50
Revises: 
Create Date: 2017-03-27 14:58:33.439728

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '985d02a40a50'
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        'reference',
	sa.Column('id', sa.BigInteger, primary_key=True),
	sa.Column('name', sa.Text, nullable=False),
	sa.Column('file', sa.Text),
	sa.Column('description', sa.Text),
	sa.Column('tiledb_reference_offset_padding_factor', sa.Float(53), server_default=sa.text(u'1.10'), nullable=False),
	sa.Column('next_tiledb_column_offset', sa.BigInteger, nullable=False, default=0, server_default=sa.text('0'))
    )

def downgrade():
    op.drop_table('reference')
