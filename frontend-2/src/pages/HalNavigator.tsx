import React, {useEffect, useState} from "react";
import {UserManager} from "oidc-client-ts";
import {klabisAuthUserManager} from "../api/setup";

const userManager: UserManager = klabisAuthUserManager;

// Generic HAL fetcher
async function fetchResource(url) {
    const user = await userManager.getUser();
    const res = await fetch(url, {
        headers: {Accept: "application/hal+json", "Authorization": `Bearer ${user?.access_token}`},
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

function HalNavigator({startUrl}) {
    const [resource, setResource] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const load = async (url) => {
        setLoading(true);
        setError(null);
        try {
            const data = await fetchResource(url);
            setResource(data);
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load(startUrl);
    }, [startUrl]);

    if (loading) return <p>Loading…</p>;
    if (error) return <p style={{color: "red"}}>Error: {error}</p>;
    if (!resource) return null;

    const links = resource._links || {};
    const embedded = resource._embedded || {};

    return (
        <div className="p-4 space-y-4">
            <h1 className="text-xl font-bold">HAL Navigator</h1>

            {/* Display resource properties */}
            <div className="p-3 border rounded bg-gray-50">
                <pre className="text-sm">{JSON.stringify(resource, null, 2)}</pre>
            </div>

            {/* Render actions based on links */}
            <div className="flex flex-wrap gap-2">
                {Object.entries(links).map(([rel, link]) => {
                    if (rel === "self") return null;
                    const href = Array.isArray(link) ? link[0].href : link.href;
                    return (
                        <button
                            key={rel}
                            className="px-3 py-1 bg-blue-500 text-white rounded shadow hover:bg-blue-600"
                            onClick={() => load(href)}
                        >
                            {rel}
                        </button>
                    );
                })}
            </div>

            {/* Render embedded collections/entities */}
            {Object.entries(embedded).map(([rel, items]) => (
                <div key={rel}>
                    <h2 className="font-semibold">{rel}</h2>
                    <ul className="list-disc list-inside">
                        {(Array.isArray(items) ? items : [items]).map((item, idx) => (
                            <li key={idx}>
                                {item.name || item.title || JSON.stringify(item)}
                                {item._links?.self && (
                                    <button
                                        className="ml-2 px-2 py-0.5 text-sm bg-gray-300 rounded"
                                        onClick={() => load(item._links.self.href)}
                                    >
                                        Open
                                    </button>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </div>
    );
}

export {HalNavigator};