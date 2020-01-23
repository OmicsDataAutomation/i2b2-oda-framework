#Copyright 2017 Omics Data Automation, Inc. All rights reserved.
"""create workspace table

Revision ID: 9467a6035f68
Revises: ba7e2f758e85
Create Date: 2017-03-28 13:53:35.219802

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '9467a6035f68'
down_revision = 'ba7e2f758e85'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
	'workspace',
	sa.Column('id', sa.BigInteger, primary_key=True),
	sa.Column('name', sa.Text, nullable=False)
    )


def downgrade():
    op.drop_table('workspace') 
