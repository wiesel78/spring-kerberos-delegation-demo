cd /opt/kerby/kdc-dist
sh ./bin/kdcinit.sh ./conf/ ./keytabs/
sh ./bin/start-kdc.sh ./conf ./runtime/
#tail -f /dev/null