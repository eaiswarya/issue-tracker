import { useParams } from 'react-router';

export function BoardPage() {
  const { key } = useParams();

  return (
    <section>
      <h1 className="text-2xl font-semibold">{key} board</h1>
    </section>
  );
}
