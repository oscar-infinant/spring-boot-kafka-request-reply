import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 2000 },
    { duration: '30s', target: 2000 },
    { duration: '1m', target: 2000 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const messageId = Math.floor(Math.random() * 100000) + 1;
  const url = `http://localhost:8090/api/send/${messageId}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is not empty': (r) => !!r.body && r.body.length > 0,
  });
  sleep(10);
}