class PromptManager:
    def __init__(self):
        self.prompts = {
            "lp_generation": """
                Create a high-converting landing page based on the following requirements:
                Company: {company_name}
                Industry: {industry}
                Target Audience: {target_audience}
                Main Goal: {goal}
                
                Generate HTML structure with modern, responsive design.
            """,
            "compliance_check": """
                Check the following content for legal compliance issues:
                Content: {content}
                
                Look for:
                - Pharmaceutical law violations
                - Misleading advertising
                - Personal information protection issues
            """,
            "optimization": """
                Analyze and optimize the following landing page for better conversion:
                Current metrics: {metrics}
                Page content: {content}
                
                Provide specific recommendations for improvement.
            """
        }
    
    def get_prompt(self, prompt_type: str, **kwargs) -> str:
        """Get formatted prompt by type"""
        template = self.prompts.get(prompt_type, "")
        return template.format(**kwargs)
    
    def add_custom_prompt(self, name: str, template: str):
        """Add custom prompt template"""
        self.prompts[name] = template