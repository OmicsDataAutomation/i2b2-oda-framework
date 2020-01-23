#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create callset_tdb_array_association

Revision ID: e5ea053fff2d
Revises: cf93474a9d08
Create Date: 2017-03-28 15:19:39.037008

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'e5ea053fff2d'
down_revision = 'cf93474a9d08'
branch_labels = None
depends_on = None

def upgrade():
    op.create_table(
	'callset_tdb_array_association',
	sa.Column('callset_id', sa.BigInteger, sa.ForeignKey('callset.id'), primary_key=True),
	sa.Column('tdb_array_id', sa.BigInteger, sa.ForeignKey('tdb_array.id'), primary_key=True),
	sa.Column('tile_row_id', sa.BigInteger),
	sa.ForeignKeyConstraint(['callset_id'],['callset.id'],),
	sa.ForeignKeyConstraint(['tdb_array_id'],['tdb_array.id'],),
	sa.PrimaryKeyConstraint('callset_id', 'tdb_array_id', name='primary_key_tdb'),
   )

def downgrade():
    op.drop_table('callset_tdb_array')
