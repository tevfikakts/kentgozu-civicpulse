"use client";

import { Component, ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class DashboardErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    console.error("[ErrorBoundary] Dashboard render hatası yakalandı:", error);
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: { componentStack: string }) {
    console.error("[ErrorBoundary] Component stack:", info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="flex min-h-screen flex-col items-center justify-center gap-4 p-8"
          style={{ background: "#09090b", color: "#f4f4f5" }}
        >
          <p className="text-lg font-semibold text-red-400">Dashboard yüklenemedi</p>
          <pre className="max-w-xl overflow-auto rounded-lg bg-zinc-900 p-4 text-xs text-zinc-300">
            {this.state.error?.message}
          </pre>
          <button
            className="rounded-md bg-zinc-800 px-4 py-2 text-sm hover:bg-zinc-700"
            onClick={() => this.setState({ hasError: false, error: null })}
          >
            Yeniden dene
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
