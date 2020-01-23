#Copyright 2017 Omics Data Automation, Inc. All rights reserved.

"""trigger for updating num rows with auto inc

Revision ID: 3504643897d0
Revises: db10c05180fc
Create Date: 2017-03-29 11:41:37.396856

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '3504643897d0'
down_revision = 'db10c05180fc'
branch_labels = None
depends_on = None


def upgrade():
    op.execute('''\
    CREATE OR REPLACE FUNCTION increment_num_rows_in_tdb_array_pgsql()
      RETURNS trigger AS $increment_num_rows_in_tdb_array_pgsql$
    DECLARE
      updated_num_rows bigint;
    BEGIN
      UPDATE tdb_array SET num_rows=
        CASE
          WHEN NEW.tile_row_id IS NULL THEN num_rows+1
          WHEN NEW.tile_row_id >= num_rows THEN NEW.tile_row_id+1
          ELSE num_rows
        END
      WHERE id=NEW.tdb_array_id RETURNING num_rows INTO updated_num_rows;
      IF NEW.tile_row_id IS NULL THEN
        NEW.tile_row_id = updated_num_rows-1;
      END IF;
      RETURN NEW;
    END;
    $increment_num_rows_in_tdb_array_pgsql$ LANGUAGE plpgsql;
    CREATE TRIGGER increment_num_rows_in_tdb_array BEFORE INSERT ON callset_tdb_array_association
    FOR EACH ROW EXECUTE PROCEDURE increment_num_rows_in_tdb_array_pgsql();
    ''')

def downgrade():
    op.execute('DROP TRIGGER increment_num_rows_in_tdb_array ON callset_tdb_array_association CASCADE')
