import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  vus: 5,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000';
const PAGE_SIZE = Number(__ENV.PAGE_SIZE || 20);

export default function () {
  const url = `${BASE_URL}/api/v1/products?page=0&size=${PAGE_SIZE}`;
  const headers = {
    'Content-Type': 'application/json',
  };

  if (__ENV.AUTH_TOKEN) {
    headers.Authorization = `Bearer ${__ENV.AUTH_TOKEN}`;
  }

  const response = http.get(url, { headers });

  const passed = check(response, {
    'status is 200': (r) => r.status === 200,
    'rate limit not exceeded': (r) => r.status !== 429,
    'response has product data': (r) => {
      try {
        const body = r.json();
        return Array.isArray(body.data);
      } catch (error) {
        return false;
      }
    },
  });

  errorRate.add(!passed);
  console.log(`Status: ${response.status}, Response time: ${response.timings.duration} ms`);

  sleep(0.1);
}
