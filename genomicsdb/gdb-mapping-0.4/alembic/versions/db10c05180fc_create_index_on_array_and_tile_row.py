#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create index on array and tile row

Revision ID: db10c05180fc
Revises: e5ea053fff2d
Create Date: 2017-03-29 11:07:31.129167

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'db10c05180fc'
down_revision = 'e5ea053fff2d'
branch_labels = None
depends_on = None


def upgrade():
    op.create_index(
        'tdb_array_id_tile_row_id_idx', 'callset_tdb_array_association',['tdb_array_id', 'tile_row_id'], unique=True
    )

def downgrade():
    op.drop_index('tdb_array_id_tile_row_id_idx', table_name='callset_tdb_array_association')
