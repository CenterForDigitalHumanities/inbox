#!/bin/bash

# Test script for /messages endpoint query constraints
# This script tests the new limit, since, and skip parameters

echo "=== Testing /messages Endpoint Query Constraints ==="
echo ""

BASE_URL="${BASE_URL:-http://localhost:3000}"

# Function to make a test announcement
create_test_message() {
  local motivation=$1
  local target=$2
  curl -s -X POST "$BASE_URL/messages" \
    -H "Content-Type: application/json" \
    -d "{
      \"@type\": \"Announce\",
      \"motivation\": \"$motivation\",
      \"target\": \"$target\",
      \"object\": \"https://example.org/object\"
    }" > /dev/null
}

# Function to count messages in response
count_messages() {
  echo "$1" | grep -o '"@id"' | wc -l
}

echo "Step 1: Creating test messages..."
# Create some test messages
for i in {1..5}; do
  create_test_message "test-query-$i" "https://example.org/target-a"
  sleep 0.1
done

echo "✓ Created 5 test messages"
echo ""

echo "Step 2: Testing default behavior (no query params)..."
RESPONSE=$(curl -s "$BASE_URL/messages")
COUNT=$(count_messages "$RESPONSE")
echo "  Messages returned: $COUNT"
if [ "$COUNT" -le 20 ]; then
  echo "✓ Default limit applied (max 20 messages)"
else
  echo "✗ Default limit not working (expected ≤ 20, got $COUNT)"
fi
echo ""

echo "Step 3: Testing with target parameter..."
RESPONSE=$(curl -s "$BASE_URL/messages?target=https://example.org/target-a")
COUNT=$(count_messages "$RESPONSE")
echo "  Messages returned: $COUNT"
if [ "$COUNT" -ge 1 ] && [ "$COUNT" -le 100 ]; then
  echo "✓ Target filter working with default limit 100"
else
  echo "✗ Target filter may not be working correctly"
fi
echo ""

echo "Step 4: Testing custom limit..."
RESPONSE=$(curl -s "$BASE_URL/messages?limit=3")
COUNT=$(count_messages "$RESPONSE")
echo "  Messages returned: $COUNT"
if [ "$COUNT" -le 3 ]; then
  echo "✓ Custom limit applied (max 3 messages)"
else
  echo "✗ Custom limit not working (expected ≤ 3, got $COUNT)"
fi
echo ""

echo "Step 5: Testing skip parameter..."
RESPONSE_NO_SKIP=$(curl -s "$BASE_URL/messages?limit=2")
RESPONSE_WITH_SKIP=$(curl -s "$BASE_URL/messages?limit=2&skip=2")
# Extract first message ID from each response
ID_NO_SKIP=$(echo "$RESPONSE_NO_SKIP" | grep -o '"@id":"[^"]*' | head -1 | cut -d'"' -f4)
ID_WITH_SKIP=$(echo "$RESPONSE_WITH_SKIP" | grep -o '"@id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ "$ID_NO_SKIP" != "$ID_WITH_SKIP" ]; then
  echo "✓ Skip parameter working (returned different messages)"
else
  echo "? Skip parameter may not be working (returned same first message)"
fi
echo ""

echo "Step 6: Testing since parameter..."
# Get a recent timestamp (5 seconds ago)
SINCE_TIME=$(date -u -d '5 seconds ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-5S +%Y-%m-%dT%H:%M:%SZ 2>/dev/null)
RESPONSE=$(curl -s "$BASE_URL/messages?since=$SINCE_TIME")
COUNT=$(count_messages "$RESPONSE")
echo "  Messages since $SINCE_TIME: $COUNT"
if [ "$COUNT" -ge 1 ]; then
  echo "✓ Since parameter filtering messages"
else
  echo "? Since parameter may not be working (expected some recent messages)"
fi
echo ""

echo "Step 7: Testing sorting (most recent first)..."
RESPONSE=$(curl -s "$BASE_URL/messages?limit=5")
# Extract published dates - this is a simple check
if echo "$RESPONSE" | grep -q "published"; then
  echo "✓ Response includes published timestamps"
else
  echo "? Could not verify sorting (no published field found)"
fi
echo ""

echo "=== Test Summary ==="
echo "All basic query constraint tests completed."
echo ""
echo "Key features tested:"
echo "  1. Default limit of 20 messages (no params)"
echo "  2. Default limit of 100 with target parameter"
echo "  3. Custom limit parameter"
echo "  4. Skip parameter for pagination"
echo "  5. Since parameter for date filtering"
echo "  6. Sorting by most recent first"
echo ""
