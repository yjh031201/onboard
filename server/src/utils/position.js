function nextPosition(lastPosition) {
  return lastPosition == null ? 1000 : lastPosition + 1000;
}

module.exports = { nextPosition };
