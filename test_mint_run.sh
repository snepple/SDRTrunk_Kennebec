#!/bin/bash
npx mintlify dev &
PID=$!
sleep 15
curl -s http://localhost:3000 > /dev/null
if [ $? -eq 0 ]; then
  echo "Mintlify server started successfully"
else
  echo "Mintlify server failed to start"
fi
kill $PID
