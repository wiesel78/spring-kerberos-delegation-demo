cd /opt/kerby/kdc-dist
sh ./bin/kdcinit.sh ./conf/ ./keytabs/
sh ./bin/start-kdc.sh ./conf ./runtime/
#tail -f /dev/null

sleep 10
echo "addprinc -pw Passw0rd123 user1@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab
echo "addprinc -pw Passw0rd123 user2@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab
echo "addprinc -pw Passw0rd123 user3@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab

echo "addprinc -randkey HTTP/frontend.iam.dev@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab
echo "addprinc -randkey HTTP/backend.iam.dev@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab

echo "ktadd -k /opt/kerby/kdc-dist/keytabs/frontend.keytab HTTP/frontend.iam.dev@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab
echo "ktadd -k /opt/kerby/kdc-dist/keytabs/backend.keytab HTTP/backend.iam.dev@IAM.DEV" | sh ./bin/kadmin.sh conf/ -k keytabs/admin.keytab
