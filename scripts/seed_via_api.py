#!/usr/bin/env python3
"""
API Data Seeder Script for Cafe POS Backend
Performs realistic demo data seeding by executing REST API HTTP requests against the backend server.
"""

import sys
import json
import urllib.request
import urllib.error

BASE_URL = "http://localhost:3000"

def make_request(url, method="GET", payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    data = json.dumps(payload).encode("utf-8") if payload else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req) as resp:
            status_code = resp.status
            response_body = resp.read().decode("utf-8")
            return status_code, json.loads(response_body) if response_body else {}
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            parsed_body = json.loads(body)
        except Exception:
            parsed_body = {"error": body}
        return e.code, parsed_body
    except Exception as ex:
        return 500, {"error": str(ex)}

def main():
    base_url = sys.argv[1] if len(sys.argv) > 1 else BASE_URL
    print(f"==================================================")
    print(f"Starting API Data Seeding against: {base_url}")
    print(f"==================================================")

    # 1. Check Server Health
    status, body = make_request(f"{base_url}/actuator/health")
    if status != 200:
        print(f"[ERROR] Health check failed with status {status}: {body}")
        print("Please ensure the Spring Boot application is running on port 3000!")
        sys.exit(1)
    print(f"[SUCCESS] Health check passed: {body.get('status', 'UP')}")

    # 2. Trigger API Data Seeding Endpoint
    print("\n--- Step 1: Triggering Database API Seed Endpoint ---")
    status, body = make_request(f"{base_url}/api/v1/seed/execute", method="POST")
    if status == 200:
        print(f"[SUCCESS] {body.get('message', 'API Data Seeding executed successfully!')}")
    else:
        print(f"[INFO] Endpoint response status {status}: {body}")

    # 3. Authenticate to get JWT Token
    print("\n--- Step 2: Authenticating User via REST API ---")
    login_payload = {
        "email": "rachel@centralperk.com",
        "password": "password123"
    }
    status, body = make_request(f"{base_url}/api/v1/auth/login", method="POST", payload=login_payload)
    
    token = None
    if status == 200 and "token" in body:
        token = body["token"]
        print(f"[SUCCESS] Logged in as rachel@centralperk.com | JWT Token received")
    else:
        print(f"[INFO] Login status {status}: {body}. Attempting signup...")
        signup_payload = {
            "email": "rachel@centralperk.com",
            "password": "password123",
            "fullName": "Rachel Green",
            "role": "OWNER"
        }
        status, body = make_request(f"{base_url}/api/v1/auth/signup", method="POST", payload=signup_payload)
        if status == 200 and "token" in body:
            token = body["token"]
            print(f"[SUCCESS] Registered & Logged in as Rachel Green | JWT Token received")

    # 4. Verify Catalog API endpoints
    if token:
        print("\n--- Step 3: Verifying Seeded Catalog via REST API ---")
        cafe_id = "00000000-0000-0000-0000-000000000001"
        status, categories = make_request(f"{base_url}/api/v1/cafes/{cafe_id}/catalog/categories", token=token)
        if status == 200 and isinstance(categories, list):
            print(f"[SUCCESS] Retreived {len(categories)} categories via REST API.")
            for cat in categories[:3]:
                print(f"  - Category: {cat.get('name')} (ID: {cat.get('id')})")
        
        status, products = make_request(f"{base_url}/api/v1/cafes/{cafe_id}/catalog/products", token=token)
        if status == 200 and isinstance(products, list):
            print(f"[SUCCESS] Retreived {len(products)} products via REST API.")
            for prod in products[:3]:
                print(f"  - Product: {prod.get('name')} | SKU: {prod.get('sku')}")

    print("\n==================================================")
    print("API Data Seeding & REST API Verification Complete!")
    print("==================================================")

if __name__ == "__main__":
    main()
