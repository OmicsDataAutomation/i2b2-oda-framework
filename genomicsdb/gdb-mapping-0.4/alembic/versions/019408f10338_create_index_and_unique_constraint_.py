# Copyright 2017 Omics Data Automation, Inc. All rights reserved.

"""create index and unique constraint reference to contig

Revision ID: 019408f10338
Revises: 3504643897d0
Create Date: 2017-03-29 12:39:49.348191

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '019408f10338'
down_revision = '3504643897d0'
branch_labels = None
depends_on = None


def upgrade():
    op.create_unique_constraint('unique_contig_name_reference_constraint', 'contig', 
        ['reference_id', 'name'])
    op.create_index('reference_id_offset_idx', 'contig', 
        ['reference_id', 'tiledb_column_offset'], 
        unique=True)

def downgrade():
    op.drop_index('reference_id_offset_idx', table_name='contig')
    op.drop_constraint('unique_contig_name_reference_constraint', 'contig', type_='unique')
