#!/usr/bin/expect -f

# ldap_stash_expect.sh
# Arg 1: LDAP service password
# Arg 2: Full path for the output stash file
# Arg 3: Distinguished Name (DN) of the LDAP service account

set ldap_password [lindex $argv 0];
set stash_file_path [lindex $argv 1];
set ldap_service_dn [lindex $argv 2];
set timeout 20

# Check if arguments are provided
if {$argc != 3} {
  puts "Usage: $argv0 <ldap_password> <stash_file_path> <ldap_service_dn>"
  exit 1
}

# Ensure the directory for the stash file exists
set stash_dir [file dirname $stash_file_path]
if {![file isdirectory $stash_dir]} {
  puts "Error: Directory $stash_dir does not exist. Please create it first."
  # Or, attempt to create it: exec mkdir -p $stash_dir
  # For Docker, it's better to ensure the directory is created in a RUN step.
  exit 1
}

spawn /usr/sbin/kdb5_ldap_util stashsrvpw -f "$stash_file_path" "$ldap_service_dn"

expect {
  timeout {
    puts "Timeout waiting for password prompt."
    exit 1
  }
  "Password for \"$ldap_service_dn\":"
}
send "$ldap_password\r"

expect {
  timeout {
    puts "Timeout waiting for password re-entry prompt."
    exit 1
  }
  "Re-enter password for \"$ldap_service_dn\":"
}
send "$ldap_password\r"

expect eof
# Catch potential errors from kdb5_ldap_util if it exits non-zero
catch wait result
exit [lindex $result 3]