# CSM Secure Widgets

CSM Secure Widgets is a toy environment for playing with instance authorization in the CSM API.  It is specifically set up for the [shared CSM design][unified_design] that will be implemented in the caBIG Clinical Trials Suite version 2.2.

[unified_design]: https://wiki.nci.nih.gov/display/Suite/Unified+security+technical+design

# Using the toy environment

## Assumptions

* a local PostgreSQL database server running on the default port
* a database named secure_widgets which is accessible by a user named "rsutphin" who has no password

If you want to use a different database server, a different user (likely) or an otherwise non-default setup for PostgreSQL, modify the properties in `hibernate.cfg.xml` and in CSM.java.

## One-time setup

Run `wipe_csm.sh` to set up the separate CSM schema and load the seed data.  You can run this again later if you want to start clean.

## Contents

The toy environment contains two runnable classes:  `Setup` and `Main`.  

`Setup` creates the application instances &mdash; it creates 512k of them, so it may take a few minutes to run the first time &mdash; and the users, roles, and privileges.

`Main` runs various tests.  It is not very fancy &mdash; the only way to change which test to run, or the parameters for a particular test, is to edit the source and recompile.  The test methods are:

* `runAssociatedWidgetsTest(int n)`: associates a user with `n + 4` protection elements via two roles.  Then it uses CSM and the applications' hibernate configuration to find the associated roles.  It prints out the time it takes to do each step of this process.
* `runGetLotsOfWidgetsTest(int count, int clauseSize)`: tests loading lots of hibernate domain objects using an IN query.

## Running the classes

The only way to run the classes right now is to set up an IDE project and run them that way, or to manually compile them via javac and run them at the command line.  (An IDEA project is included.)

# TODO

* Scripts to set up the database
* A single place to set the database access credentials
* Script to run Main