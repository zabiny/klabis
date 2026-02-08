export function JsonPreview({data, label = "Data"}: { data?: unknown, label?: string }) {
    return <div><h2>{label}</h2>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>;
}