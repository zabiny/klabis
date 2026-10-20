export interface FeeSummaryGroup {
    id: string;
    name: string;
    yearlyFee: number;
}

export interface FeeSummaryLinks {
    self: {href: string};
    feeHistory?: {href: string};
}

export interface FeeSummaryData {
    currentGroup: FeeSummaryGroup | null;
    votingOpen: boolean;
    recommendedGroupId: string | null;
    _links: FeeSummaryLinks;
}
