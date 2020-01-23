#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create callset table

Revision ID: cf93474a9d08
Revises: b041bbac855a
Create Date: 2017-03-28 15:04:08.770828

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.schema import CreateSequence, Sequence

# revision identifiers, used by Alembic.
revision = 'cf93474a9d08'
down_revision = 'b041bbac855a'
branch_labels = None
depends_on = None


def upgrade():
    op.execute(CreateSequence(Sequence('callset_id_seq', minvalue=0, start=0)))
    op.create_table(
	'callset',
	sa.Column('id', sa.BigInteger, Sequence('callset_id_seq'), primary_key=True),
	sa.Column('name', sa.Text, nullable=False),
	sa.Column('description', sa.Text)
    )

def downgrade():
    op.execute(DropSequence('callset_id_seq'))
    op.drop_table('callset')
