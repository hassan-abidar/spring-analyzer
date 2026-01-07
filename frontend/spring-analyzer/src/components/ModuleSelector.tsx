import React from 'react';
import { Layers } from 'lucide-react';
import './ModuleSelector.css';

interface ModuleSelectorProps {
  modules: string[];
  selectedModule: string | null;
  onModuleChange: (module: string | null) => void;
  moduleSummaries?: Record<string, { totalClasses: number; endpoints: number }>;
}

const ModuleSelector: React.FC<ModuleSelectorProps> = ({
  modules,
  selectedModule,
  onModuleChange,
  moduleSummaries
}) => {
  if (modules.length <= 1) {
    return null;
  }

  return (
    <div className="module-selector">
      <div className="module-selector-header">
        <Layers size={16} />
        <span>Filter by Module</span>
      </div>
      <div className="module-pills">
        <button
          className={`module-pill ${selectedModule === null ? 'active' : ''}`}
          onClick={() => onModuleChange(null)}
        >
          All Modules
          <span className="module-count">{modules.length}</span>
        </button>
        {modules.map(module => (
          <button
            key={module}
            className={`module-pill ${selectedModule === module ? 'active' : ''}`}
            onClick={() => onModuleChange(module)}
          >
            {module}
            {moduleSummaries && moduleSummaries[module] && (
              <span className="module-count">
                {moduleSummaries[module].totalClasses}
              </span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
};

export default ModuleSelector;
