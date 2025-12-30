/**
 * Reusable component for displaying HAL resource links
 * Shows available actions grouped by link relationship
 * Automatically uses resourceData._links if links prop is not provided
 */

import {type ReactElement, useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../../contexts/HalRouteContext';
import {HAL_LINK_RELS} from '../../constants/hal.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {extractNavigationPath} from '../../utils/navigationPath';
import {linkSectionStyles} from '../../theme/designTokens';

interface HalLinksSectionProps {
	/** Links object from HAL resource. If not provided, uses resourceData._links */
	links?: Record<string, any>;
	/** Callback when a link is clicked. If not provided, uses useNavigate from React Router */
	onNavigate?: (href: string) => void;
}

/**
 * Component to display available links/actions from a HAL resource
 * Filters out self-links and renders clickable buttons for navigation
 *
 * If `links` is not provided, automatically fetches from useHalRoute()._links
 * If `onNavigate` is not provided, uses React Router's useNavigate() hook
 *
 * @example
 * // Automatic - uses resourceData._links and React Router navigation
 * <HalLinksSection />
 *
 * @example
 * // Manual - provides explicit links and handler
 * <HalLinksSection
 *   links={customLinks}
 *   onNavigate={(href) => handleCustomNavigation(href)}
 * />
 */
export function HalLinksSection({
									links: propsLinks,
									onNavigate: propsOnNavigate
								}: HalLinksSectionProps): ReactElement | null {
	const {resourceData} = useHalRoute();
	const routerNavigate = useNavigate();

	// Use provided links or fallback to resourceData._links
	const links = propsLinks || resourceData?._links;

	// Memoize default navigation callback to prevent unnecessary re-renders
	const defaultNavigate = useCallback((href: string) => {
		const path = extractNavigationPath(href);
		routerNavigate(path);
	}, [routerNavigate]);

	// Use provided callback or the memoized default
	const onNavigate = propsOnNavigate || defaultNavigate;

	if (!links || Object.keys(links).length === 0) {
		return null;
	}

	const displayLinks = Object.entries(links)
		.filter((([rel]) => rel !== HAL_LINK_RELS.SELF));

	if (displayLinks.length === 0) {
		return null;
	}

	return (
		<div className={linkSectionStyles.container}>
			<h3 className={linkSectionStyles.heading}>{UI_MESSAGES.AVAILABLE_ACTIONS}</h3>
			<div className={linkSectionStyles.buttonContainer}>
				{displayLinks
					.map(([rel, link]: [string, any]) => {
						const linkArray = Array.isArray(link) ? link : [link];
						return linkArray.map((l: any, idx: number) => (
							<button
								key={`${rel}-${idx}`}
								onClick={() => onNavigate(l.href)}
								className={linkSectionStyles.linkButton}
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
