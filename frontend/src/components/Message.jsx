export default function Message({ text, isError }) {
  if (!text) {
    return null;
  }

  return (
    <div className={`message${isError ? " error" : ""}`}>{text}</div>
  );
}
