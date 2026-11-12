# 查看本机 PostgreSQL 状态
& 'D:\pg\pgsql\bin\pg_isready.exe' -h 127.0.0.1 -p 5432
"ISREADY_EXIT=$LASTEXITCODE"
& 'D:\pg\pgsql\bin\pg_ctl.exe' -D 'D:\pgdata' status
