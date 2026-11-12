# 停止本机便携版 PostgreSQL
& 'D:\pg\pgsql\bin\pg_ctl.exe' -D 'D:\pgdata' -m fast stop
exit $LASTEXITCODE
