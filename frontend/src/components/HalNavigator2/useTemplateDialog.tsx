import {type ReactElement, type ReactNode, useCallback, useState} from 'react';
import {createPortal} from 'react-dom';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {getDialogTitleLabel, getTemplateLabel} from '../../localization';
import {Modal} from '../UI';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import type {HalFormFieldFactory} from './halforms';

export interface TemplateDialogOptions {
    resource?: HalResponse | null;
    title?: string | ((template: HalFormsTemplate) => string);
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
    note?: ReactNode;
    submitLabel?: string;
    successMessage?: string;
    onSubmitSuccess?: (responseData?: unknown) => void;
    onSubmitError?: (error: unknown) => boolean | void;
    navigateOnSuccess?: boolean;
    fieldsFactory?: HalFormFieldFactory;
    postprocessPayload?: (payload: Record<string, unknown>, template: HalFormsTemplate) => Record<string, unknown>;
}

export interface TemplateDialog {
    available: boolean;
    availableOn: (resource: HalResponse | null | undefined, name?: string) => boolean;
    label: string;
    open: (resource?: HalResponse | null, name?: string) => void;
    dialog: ReactElement | null;
}

interface OpenDialog {
    resource: HalResponse;
    name: string;
}

const templateOf = (resource: HalResponse | null | undefined, name: string | undefined): HalFormsTemplate | undefined =>
    name ? (resource?._templates as Record<string, HalFormsTemplate> | undefined)?.[name] : undefined;

const selfHref = (resource: HalResponse): string | undefined => {
    const self = resource._links?.self;
    const link = Array.isArray(self) ? self[0] : self;
    return typeof link?.href === 'string' ? link.href : undefined;
};

export function useTemplateDialog(name?: string, options: TemplateDialogOptions = {}): TemplateDialog {
    const {route: {pathname}} = useHalPageData();
    const [opened, setOpened] = useState<OpenDialog | null>(null);
    const {resource: defaultResource} = options;

    const availableOn = useCallback(
        (resource: HalResponse | null | undefined, templateName?: string) =>
            templateOf(resource, templateName ?? name) !== undefined,
        [name],
    );

    const open = useCallback((resource?: HalResponse | null, templateName?: string) => {
        const target = resource ?? defaultResource;
        const targetName = templateName ?? name;
        if (target && targetName && templateOf(target, targetName)) {
            setOpened({resource: target, name: targetName});
        }
    }, [defaultResource, name]);

    const close = useCallback(() => setOpened(null), []);

    const defaultTemplate = templateOf(defaultResource, name);
    const label = name && defaultTemplate ? (getTemplateLabel(name) ?? defaultTemplate.title ?? name) : '';

    const openedTemplate = opened ? templateOf(opened.resource, opened.name) : undefined;
    let dialog: ReactElement | null = null;
    if (opened && openedTemplate) {
        const title = typeof options.title === 'function'
            ? options.title(openedTemplate)
            : options.title ?? getDialogTitleLabel(opened.name) ?? getTemplateLabel(opened.name) ?? openedTemplate.title;

        dialog = createPortal(
            <Modal isOpen onClose={close} title={title} size={options.size ?? 'md'} context={options.note}>
                <HalFormDisplay
                    template={openedTemplate}
                    templateName={opened.name}
                    resourceData={opened.resource as Record<string, unknown>}
                    pathname={pathname}
                    resourceUrl={selfHref(opened.resource)}
                    onClose={close}
                    onSubmitSuccess={(data) => {
                        options.onSubmitSuccess?.(data);
                        close();
                    }}
                    onSubmitError={options.onSubmitError}
                    successMessage={options.successMessage}
                    submitButtonLabel={options.submitLabel}
                    navigateOnSuccess={options.navigateOnSuccess}
                    fieldsFactory={options.fieldsFactory}
                    postprocessPayload={options.postprocessPayload}
                />
            </Modal>,
            document.body,
        );
    }

    return {available: availableOn(defaultResource, name), availableOn, label, open, dialog};
}
