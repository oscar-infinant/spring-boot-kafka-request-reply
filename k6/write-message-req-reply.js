import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '10s', target: 1500 },
    { duration: '30s', target: 1500 },
    { duration: '1m', target: 1500 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const url = 'http://localhost:8090/api/send';
  const uuid = uuidv4();
  const payload = JSON.stringify(`message-${uuid}`);

  const params = {
    headers: {
      'Content-Type': 'text/plain',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(10);
}