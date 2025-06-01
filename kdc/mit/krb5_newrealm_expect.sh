#!/usr/bin/expect -f

set master_password [lindex $argv 0];
# set realm [lindex $argv 1];
set timeout 20

spawn /usr/sbin/krb5_newrealm

expect "Enter KDC database master key:"
send "$master_password\r"

expect "Re-enter KDC database master key to verify:"
send "$master_password\r"

# Allow krb5_newrealm to complete its operations
expect eof