#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""increment for tiledb offset trigger function

Revision ID: eaff19134b73
Revises: 019408f10338
Create Date: 2017-03-29 13:04:27.508782

"""
from alembic import op
import sqlalchemy as sa

tiledb_reference_offset_padding_factor_default = 1.1

def get_tiledb_padded_reference_length_string(reference_length_str):
      return (
                  'CAST( CAST(%s AS DOUBLE PRECISION)*(SELECT tiledb_reference_offset_padding_factor FROM reference WHERE id=NEW.reference_id) AS BIGINT)' %
                          (reference_length_str))

      def get_tiledb_padded_reference_length_string_default(reference_length_str):
            return ('CAST( CAST(%s AS DOUBLE PRECISION)*%.1f AS BIGINT)' %
                            (reference_length_str, tiledb_reference_offset_padding_factor_default))

# revision identifiers, used by Alembic.
revision = 'eaff19134b73'
down_revision = '019408f10338'
branch_labels = None
depends_on = None


def upgrade():
  padding = get_tiledb_padded_reference_length_string('NEW.length')
  op.execute('''\
    CREATE OR REPLACE FUNCTION increment_next_column_in_reference_pgsql()
      RETURNS trigger AS $increment_next_column_in_reference_pgsql$
    DECLARE
      updated_next_tiledb_column_offset bigint;
      padded_reference_length bigint;
    BEGIN
      padded_reference_length = %s;
      UPDATE reference SET next_tiledb_column_offset=
        CASE
          WHEN NEW.tiledb_column_offset IS NULL THEN next_tiledb_column_offset+padded_reference_length
          WHEN NEW.tiledb_column_offset+padded_reference_length>next_tiledb_column_offset THEN NEW.tiledb_column_offset+padded_reference_length
          ELSE next_tiledb_column_offset
        END
      WHERE id = NEW.reference_id RETURNING next_tiledb_column_offset INTO updated_next_tiledb_column_offset;
      IF NEW.tiledb_column_offset IS NULL THEN
        NEW.tiledb_column_offset = updated_next_tiledb_column_offset-padded_reference_length;
      END IF;                                                                                                                                                     RETURN NEW;
    END;
    $increment_next_column_in_reference_pgsql$ LANGUAGE plpgsql;
    CREATE TRIGGER increment_next_column_in_reference BEFORE INSERT ON contig                                                                                   FOR EACH ROW EXECUTE PROCEDURE increment_next_column_in_reference_pgsql();
    ''' % (padding))

def downgrade():
  op.execute('DROP TRIGGER increment_next_column_in_reference ON contig CASCADE')

