/**
 * Reusable component for displaying HAL resource links
 * Shows available actions grouped by link relationship
 */

import {type ReactElement} from 'react';
import {HAL_LINK_RELS} from '../../constants/hal.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';

interface HalLinksSectionProps {
	links?: Record<string, any>;
	onNavigate: (href: string) => void;
}

/**
 * Component to display available links/actions from a HAL resource
 * Filters out self-links and renders clickable buttons for navigation
 */
export function HalLinksSection({links, onNavigate}: HalLinksSectionProps): ReactElement | null {
	if (!links || Object.keys(links).length === 0) {
		return null;
	}

	const displayLinks = Object.entries(links)
		.filter((([rel]) => rel !== HAL_LINK_RELS.SELF));

	if (displayLinks.length === 0) {
		return <></>;
	}

	return (
		<div className="mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900">
			<h3 className="font-semibold mb-2">{UI_MESSAGES.AVAILABLE_ACTIONS}</h3>
			<div className="flex flex-wrap gap-2">
				{displayLinks
					.map(([rel, link]: [string, any]) => {
						const linkArray = Array.isArray(link) ? link : [link];
						return linkArray.map((l: any, idx: number) => (
							<button
								key={`${rel}-${idx}`}
								onClick={() => onNavigate(l.href)}
								className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer"
								title={rel}
							>
								{l.title || rel}
							</button>
						));
					})}
			</div>
		</div>
	);
}
