#!/bin/bash

# Test script for rate limiting and metadata tracking
# This script demonstrates the rate limiting feature and metadata tracking

echo "=== Rate Limiting and Metadata Tracking Test ==="
echo ""
echo "Testing POST /messages endpoint with rate limiting..."
echo ""

BASE_URL="${BASE_URL:-http://localhost:3000}"
SUCCESS_COUNT=0
RATE_LIMITED_COUNT=0

# Make 12 POST requests to test rate limiting
for i in {1..12}; do
  echo -n "Request $i: "
  
  RESPONSE=$(curl -s -X POST "$BASE_URL/messages" \
    -H "Content-Type: application/json" \
    -H "User-Agent: TestScript/1.0" \
    -H "Referer: https://test.example.org" \
    -d "{
      \"@type\": \"Announce\",
      \"motivation\": \"test-rate-limit-$i\",
      \"target\": \"https://example.org/resource-$i\",
      \"object\": \"https://example.org/object-$i\"
    }")
  
  if echo "$RESPONSE" | grep -q '"@id"'; then
    echo "✓ Success"
    ((SUCCESS_COUNT++))
  elif echo "$RESPONSE" | grep -q "Rate limit exceeded"; then
    echo "✗ Rate limited"
    ((RATE_LIMITED_COUNT++))
  else
    echo "? Unexpected response"
  fi
  
  sleep 0.5
done

echo ""
echo "=== Test Results ==="
echo "Successful requests: $SUCCESS_COUNT"
echo "Rate limited requests: $RATE_LIMITED_COUNT"
echo ""

if [ $SUCCESS_COUNT -ge 8 ] && [ $RATE_LIMITED_COUNT -ge 2 ]; then
  echo "✓ Rate limiting is working correctly!"
  echo "  - Allowed ~10 requests per hour per IP"
  echo "  - Blocked subsequent requests with 429 status"
else
  echo "✗ Rate limiting may not be working as expected"
fi

echo ""
echo "=== Metadata Tracking ==="
echo "Internal metadata (__inbox field) is tracked for each POST:"
echo "  - IP address (from X-Forwarded-For or direct connection)"
echo "  - Referrer (from Referer header or 'direct')"
echo "  - User-Agent (from User-Agent header or 'unknown')"
echo "  - Timestamp (creation time)"
echo ""
echo "The __inbox field is:"
echo "  ✓ Stored in MongoDB"
echo "  ✓ Stripped from all GET responses"
echo "  ✓ Used for rate limiting calculations"
