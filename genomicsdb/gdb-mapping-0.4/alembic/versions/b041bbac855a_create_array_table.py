#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create array table

Revision ID: b041bbac855a
Revises: 9467a6035f68
Create Date: 2017-03-28 14:02:51.728734

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'b041bbac855a'
down_revision = '9467a6035f68'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
	'tdb_array',
	sa.Column('id', sa.BigInteger, primary_key=True),
	sa.Column('reference_id', sa.BigInteger, sa.ForeignKey('reference.id'), nullable=False),
	sa.Column('workspace_id', sa.BigInteger, sa.ForeignKey('workspace.id'), nullable=False),
	sa.Column('name', sa.Text, nullable=False),
	sa.Column('num_rows', sa.BigInteger, nullable=False, default=0, server_default=sa.text('0'))
    )

def downgrade():
    op.drop_table('tdb_array')
